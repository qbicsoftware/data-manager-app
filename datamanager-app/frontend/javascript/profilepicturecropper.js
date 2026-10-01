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

    const controls = document.createElement('div');
    controls.className = 'profile-picture-crop__controls';
    controls.append(choose, preview);

    const stage = document.createElement('div');
    stage.className = 'profile-picture-crop__stage';
    const image = document.createElement('img');
    image.className = 'profile-picture-crop__image';
    image.alt = '';
    stage.appendChild(image);

    const hint = document.createElement('span');
    hint.className = 'profile-picture-crop__hint';
    hint.textContent = 'PNG or JPEG, max 1 MB. Drag to reposition, zoom with the scroll wheel.';

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

    input.addEventListener('change', () => {
      const file = input.files && input.files[0];
      if (!file) {
        return;
      }
      if (file.size > 1024 * 1024) {
        host.dispatchEvent(new CustomEvent('qbic-crop-error', {
          detail: 'TOO_LARGE',
          bubbles: true,
        }));
        input.value = '';
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        const existing = croppers.get(cropperKey);
        if (existing) {
          existing.replace(reader.result);
        } else {
          image.src = reader.result;
          croppers.set(cropperKey, new Cropper(image, {
            aspectRatio: 1,
            viewMode: 1,
            dragMode: 'move',
            autoCropArea: 1,
            background: false,
            guides: false,
            crop: schedulePreview,
            ready: renderPreview,
          }));
        }
        host.classList.add('has-image');
        renderPreview();
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
