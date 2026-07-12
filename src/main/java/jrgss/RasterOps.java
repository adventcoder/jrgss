package jrgss;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

//NOTE: blurs are done at raster level, and assume premultiplied alpha for correctness
//TODO: we should grab pixels one scanline at a time rather than one pixel at a time, this would eliminate the need for a scratch buffer for blurX/blurY
public class RasterOps {
    public static void blurX(Raster src, WritableRaster dst, int radius) {
        float weight = 1.0f / (2*radius + 1);
        int[] pixel = new int[src.getNumBands()];
        float newPixel[] = new float[pixel.length];

        for (int y = 0; y < dst.getHeight(); y++) {
            Arrays.fill(newPixel, 0.0f);
            for (int x = -radius; x < radius; x++) {
                src.getPixel(wrap(x, src.getWidth()), y, pixel);
                for (int i = 0; i < pixel.length; i++)
                    newPixel[i] += pixel[i]*weight;
            }

            for (int x = 0; x < dst.getWidth(); x++) {
                src.getPixel(wrap(x + radius, src.getWidth()), y, pixel);
                for (int i = 0; i < pixel.length; i++)
                    newPixel[i] += pixel[i]*weight;

                dst.setPixel(x, y, newPixel);

                src.getPixel(wrap(x - radius, src.getWidth()), y, pixel);
                for (int i = 0; i < pixel.length; i++)
                    newPixel[i] -= pixel[i]*weight;
            }
        }
    }

    public static void blurY(Raster src, WritableRaster dst, int radius) {
        float weight = 1.0f / (2*radius + 1);
        int[] pixel = new int[src.getNumBands()];
        float newPixel[] = new float[pixel.length];

        for (int x = 0; x < dst.getWidth(); x++) {
            Arrays.fill(newPixel, 0.0f);
            for (int y = -radius; y < radius; y++) {
                src.getPixel(x, wrap(y, src.getHeight()), pixel);
                for (int i = 0; i < pixel.length; i++)
                    newPixel[i] += pixel[i]*weight;
            }

            for (int y = 0; y < dst.getHeight(); y++) {
                src.getPixel(x, wrap(y + radius, src.getHeight()), pixel);
                for (int i = 0; i < pixel.length; i++)
                    newPixel[i] += pixel[i]*weight;

                dst.setPixel(x, y, newPixel);

                src.getPixel(x, wrap(y - radius, src.getHeight()), pixel);
                for (int i = 0; i < pixel.length; i++)
                    newPixel[i] -= pixel[i]*weight;
            }
        }
    }

    public static void radialBlur(Raster src, WritableRaster dst, int amplitude, int division) {
        float originX = src.getWidth() * 0.5f;
        float originY = src.getHeight() * 0.5f;
        float weight = 1.0f / division;

        float[] cos = new float[division];
        float[] sin = new float[division];
        // split evenly from -amplitude/2 -> amplitude/2 including both endpoints
        for (int d = 0; d < division; d++) {
            double t = ((double) d) / (division - 1);
            double rads = Math.toRadians(amplitude * (t - 0.5));
            cos[d] = (float) Math.cos(rads);
            sin[d] = (float) Math.sin(rads);
        }

        int[] pixel = new int[src.getNumBands()];
        float newPixel[] = new float[pixel.length];

        for (int y = 0; y < dst.getHeight(); y++) {
            for (int x = 0; x < dst.getWidth(); x++) {
                Arrays.fill(newPixel, 0.0f);

                float dx = x + 0.5f - originX;
                float dy = y + 0.5f - originY;
                for (int d = 0; d < division; d++) {
                    float newX = dx*cos[d] - dy*sin[d] + originX;
                    float newY = dx*sin[d] + dy*cos[d] + originY;

                    samplePixel(src, newX, newY, pixel);
                    for (int i = 0; i < pixel.length; i++)
                        newPixel[i] += pixel[i]*weight;
                }

                dst.setPixel(x, y, newPixel);
            }
        }
    }

    private static void samplePixel(Raster src, double x, double y, int[] pixel) {
        int srcX = wrap((int) Math.floor(x), src.getWidth());
        int srcY = wrap((int) Math.floor(y), src.getHeight());
        src.getPixel(srcX, srcY, pixel);
    }

    private static int wrap(int i, int size) {
        if (i < 0) return -i - 1;
        if (i >= size) return 2*size - i - 1;
        return i;
    }
}
