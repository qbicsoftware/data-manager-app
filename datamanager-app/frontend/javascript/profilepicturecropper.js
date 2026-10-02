/*
 * Profile picture cropper.
 *
 * Wraps Cropper.js (classic v1 API) for a square crop frame with a live circular preview.
 * The cropped result is returned to the server as a 256x256 PNG data URL. The server always
 * re-validates and re-encodes on upload, so this client-side output is only a convenience.
 *
 * Exposed as window.qbicProfilePictureCropper = { init(host), export(host) }.
 */
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';

let cropperCounter = 0;
const croppers = new Map();

window.qbicProfilePictureCropper = {
  init(host) {
    if (!host || host.__qbicCropperInit) {
      return;
    }
    host.__qbicCropperInit = true;
    const cropperKey = `qbic-cropper-${cropperCounter++}`;
    host.__qbicCropperKey = cropperKey;

    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/png,image/jpeg';
    input.className = 'profile-picture-crop__input';

    const choose = document.createElement('label');
    choose.className = 'profile-picture-crop__choose';
    choose.textContent = 'Choose image';
    choose.appendChild(input);

    const preview = document.createElement('div');
    preview.className = 'profile-picture-crop__preview';
    preview.setAttribute('aria-hidden', 'true');

    const previewLabel = document.createElement('span');
    previewLabel.className = 'profile-picture-crop__preview-label';
    previewLabel.textContent = 'Preview';

    const previewWrap = document.createElement('div');
    previewWrap.className = 'profile-picture-crop__preview-wrap';
    previewWrap.append(preview, previewLabel);

    const controls = document.createElement('div');
    controls.className = 'profile-picture-crop__controls';
    controls.append(choose, previewWrap);

    const stage = document.createElement('div');
    stage.className = 'profile-picture-crop__stage';
    const image = document.createElement('img');
    image.className = 'profile-picture-crop__image';
    image.alt = '';
    stage.appendChild(image);

    const hint = document.createElement('span');
    hint.className = 'profile-picture-crop__hint';
    hint.textContent = 'PNG or JPEG, max 1 MB. Drag to reposition, resize with the corner '
        + 'handles, zoom with the scroll wheel.';

    host.append(controls, stage, hint);

    const renderPreview = () => {
      const cropper = croppers.get(cropperKey);
      if (!cropper) {
        return;
      }
      const canvas = cropper.getCroppedCanvas({ width: 96, height: 96 });
      if (canvas) {
        preview.style.backgroundImage = `url(${canvas.toDataURL('image/png')})`;
      }
    };

    let scheduled = false;
    const schedulePreview = () => {
      if (scheduled) {
        return;
      }
      scheduled = true;
      window.requestAnimationFrame(() => {
        scheduled = false;
        renderPreview();
      });
    };

    const notifyError = (reason) => {
      if (host.$server && host.$server.onValidationError) {
        host.$server.onValidationError(reason);
      }
    };

    const clearError = () => {
      if (host.$server && host.$server.clearValidationError) {
        host.$server.clearValidationError();
      }
    };

    input.addEventListener('change', () => {
      const file = input.files && input.files[0];
      if (!file) {
        return;
      }
      if (file.size > 1024 * 1024) {
        notifyError('TOO_LARGE');
        input.value = '';
        return;
      }
      if (file.type && file.type !== 'image/png' && file.type !== 'image/jpeg') {
        notifyError('UNSUPPORTED_FORMAT');
        input.value = '';
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        // Validate the decoded pixel count before handing a potential decompression bomb to
        // Cropper, which would otherwise try to render it.
        const probe = new Image();
        probe.onload = () => {
          if (probe.naturalWidth * probe.naturalHeight > 16000000) {
            notifyError('TOO_MANY_PIXELS');
            input.value = '';
            return;
          }
          const existing = croppers.get(cropperKey);
          if (existing) {
            existing.replace(reader.result);
          } else {
            image.src = reader.result;
            croppers.set(cropperKey, new Cropper(image, {
              aspectRatio: 1,
              viewMode: 1,
              dragMode: 'move',
              // Start slightly smaller than the image so the crop frame and handles are always
              // visible — a square source with autoCropArea 1 would hide them at the image edge.
              autoCropArea: 0.85,
              background: false,
              guides: true,
              crop: schedulePreview,
              ready: renderPreview,
            }));
          }
          host.classList.add('has-image');
          clearError();
          renderPreview();
        };
        probe.onerror = () => {
          notifyError('UNSUPPORTED_FORMAT');
          input.value = '';
        };
        probe.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  },

  export(host) {
    if (!host || !host.__qbicCropperInit) {
      return null;
    }
    const cropper = croppers.get(host.__qbicCropperKey);
    if (!cropper) {
      return null;
    }
    const canvas = cropper.getCroppedCanvas({
      width: 256,
      height: 256,
      imageSmoothingQuality: 'high',
    });
    return canvas ? canvas.toDataURL('image/png') : null;
  },
};
