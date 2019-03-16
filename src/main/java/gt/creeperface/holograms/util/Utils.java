package gt.creeperface.holograms.util;

import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author CreeperFace
 */
@UtilityClass
public class Utils {

    public static int[] readFontTexture(InputStream imageStream, int[] charWidths) throws IOException {
        if (charWidths == null) {
            charWidths = new int[256];
        }


        BufferedImage bufferedimage = ImageIO.read(imageStream);

        int imgWidth = bufferedimage.getWidth();
        int imgHeight = bufferedimage.getHeight();

        int[] pixels = new int[imgWidth * imgHeight];

        bufferedimage.getRGB(0, 0, imgWidth, imgHeight, pixels, 0, imgWidth);
        int charHeight = imgHeight / 16;
        int charWidth = imgWidth / 16;

        float width = 8F / charWidth;

        for (int i = 0; i < 256; ++i) {
            int column = i % 16;
            int row = i / 16;

            if (i == 32) {
                charWidths[i] = 4;
            }

            int hGap;

            for (hGap = charWidth - 1; hGap >= 0; --hGap) {
                int colOffset = column * charWidth + hGap;
                boolean transparent = true;

                for (int vGap = 0; vGap < charHeight && transparent; ++vGap) {
                    int rowOffset = (row * charWidth + vGap) * imgWidth;

                    if ((pixels[colOffset + rowOffset] >> 24 & 255) != 0) {
                        transparent = false;
                    }
                }

                if (!transparent) {
                    break;
                }
            }

            ++hGap;
            charWidths[i] = (int) (0.5 + (hGap * width)) + 1;
        }

        return charWidths;
    }

}
