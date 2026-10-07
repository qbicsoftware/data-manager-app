package life.qbic.datamanager.profilepicture

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import spock.lang.Specification

class ImageNormalizerSpec extends Specification {

    ImageNormalizer normalizer = new ImageNormalizer()

    def "normalizes a PNG to a 256x256 PNG derivative with a stable hash"() {
        given:
        byte[] upload = imageBytes(640, 480, "png")

        when:
        def result = normalizer.normalize(upload)

        then:
        result.width() == ImageNormalizer.TARGET_SIZE
        result.height() == ImageNormalizer.TARGET_SIZE
        result.contentHash() =~ /^[0-9a-f]{64}$/
        isPng(result.png())

        and: "re-normalizing the same source yields the same hash"
        normalizer.normalize(upload).contentHash() == result.contentHash()
    }

    def "normalizes a JPEG to a PNG derivative"() {
        given:
        byte[] upload = imageBytes(800, 600, "jpg")

        when:
        def result = normalizer.normalize(upload)

        then:
        result.width() == ImageNormalizer.TARGET_SIZE
        result.height() == ImageNormalizer.TARGET_SIZE
        isPng(result.png())
    }

    def "uses an explicit square crop frame"() {
        given:
        byte[] upload = imageBytes(500, 500, "png")

        when:
        def result = normalizer.normalize(upload, new ImageNormalizer.CropFrame(100, 50, 200))

        then:
        result.width() == ImageNormalizer.TARGET_SIZE
    }

    def "rejects an upload larger than the size limit"() {
        given: "a byte array just over the limit (size is checked before format)"
        byte[] upload = new byte[(int) ImageNormalizer.MAX_UPLOAD_BYTES + 1]
        upload[0] = (byte) 0x89

        when:
        normalizer.normalize(upload)

        then:
        def e = thrown(ImageNormalizationException)
        e.reason() == ImageNormalizationException.Reason.TOO_LARGE
    }

    def "rejects unsupported formats even with a valid image extension"() {
        given:
        byte[] upload = "GIF89a not really an image".getBytes("UTF-8")

        when:
        normalizer.normalize(upload)

        then:
        def e = thrown(ImageNormalizationException)
        e.reason() == ImageNormalizationException.Reason.UNSUPPORTED_FORMAT
    }

    def "rejects corrupted PNG bytes"() {
        given: "a valid PNG magic header followed by garbage"
        byte[] upload = new byte[64]
        byte[] magic = [(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                        (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A]
        System.arraycopy(magic, 0, upload, 0, magic.length)

        when:
        normalizer.normalize(upload)

        then:
        def e = thrown(ImageNormalizationException)
        e.reason() == ImageNormalizationException.Reason.CORRUPT
    }

    def "rejects animated PNG content (acTL chunk)"() {
        given: "a valid PNG with an acTL marker appended"
        byte[] png = imageBytes(100, 100, "png")
        byte[] upload = concat(png, "acTL".getBytes("US-ASCII"))

        when:
        normalizer.normalize(upload)

        then:
        def e = thrown(ImageNormalizationException)
        e.reason() == ImageNormalizationException.Reason.ANIMATED
    }

    def "rejects a decompression bomb before scaling"() {
        given: "a solid image whose decoded pixel count exceeds the guard (file stays small)"
        int edge = (int) Math.ceil(Math.sqrt((double) ImageNormalizer.MAX_DECODED_PIXELS)) + 1
        byte[] upload = imageBytes(edge, edge, "png")

        expect:
        upload.length <= ImageNormalizer.MAX_UPLOAD_BYTES

        when:
        normalizer.normalize(upload)

        then:
        def e = thrown(ImageNormalizationException)
        e.reason() == ImageNormalizationException.Reason.TOO_MANY_PIXELS
    }

    def "rejects a crop frame outside the source image"() {
        given:
        byte[] upload = imageBytes(200, 200, "png")

        when:
        normalizer.normalize(upload, new ImageNormalizer.CropFrame(150, 0, 100))

        then:
        def e = thrown(ImageNormalizationException)
        e.reason() == ImageNormalizationException.Reason.INVALID_CROP
    }

    private static byte[] imageBytes(int width, int height, String format) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        Graphics2D graphics = image.createGraphics()
        graphics.setColor(Color.BLUE)
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ImageIO.write(image, format, out)
        return out.toByteArray()
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length]
        System.arraycopy(first, 0, result, 0, first.length)
        System.arraycopy(second, 0, result, first.length, second.length)
        return result
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length > 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == (byte) 'P'
                && bytes[2] == (byte) 'N'
                && bytes[3] == (byte) 'G'
    }
}
