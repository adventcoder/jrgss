package jrgss;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

//NOTE: blurs are done at raster level, and assume premultiplied alpha for correctness
public class RasterOps {
    public static void blurX(Raster src, WritableRaster dst, int radius) {
        double weight = 1.0f / (2*radius + 1);
        int[] pixel = new int[src.getNumBands()];
        double newPixel[] = new double[pixel.length];

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
        double weight = 1.0f / (2*radius + 1);
        int[] pixel = new int[src.getNumBands()];
        double newPixel[] = new double[pixel.length];

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

    public static void radialBlur(Raster src, WritableRaster dst, int amplitude, int divisions) {
        double originX = src.getWidth() * 0.5;
        double originY = src.getHeight() * 0.5;

        double[] cos = new double[divisions];
        double[] sin = new double[divisions];
        for (int d = 0; d < divisions; d++) {
            double t = ((double) d) / (divisions - 1);
            double rads = Math.toRadians(amplitude * (t - 0.5));
            cos[d] = Math.cos(rads);
            sin[d] = Math.sin(rads);
        }

        double weight = 1.0 / divisions;
        int[] pixel = new int[src.getNumBands()];
        double newPixel[] = new double[pixel.length];

        for (int y = 0; y < dst.getHeight(); y++) {
            for (int x = 0; x < dst.getWidth(); x++) {
                Arrays.fill(newPixel, 0.0);

                double dx = x + 0.5 - originX;
                double dy = y + 0.5 - originY;
                for (int d = 0; d < divisions; d++) {
                    int newX = (int) Math.floor(dx*cos[d] - dy*sin[d] + originX);
                    int newY = (int) Math.floor(dx*sin[d] + dy*cos[d] + originY);

                    src.getPixel(wrap(newX, src.getWidth()), wrap(newY, src.getHeight()), pixel);
                    for (int i = 0; i < pixel.length; i++)
                        newPixel[i] += pixel[i]*weight;
                }

                dst.setPixel(x, y, newPixel);
            }
        }
    }

    private static int wrap(int i, int size) {
        if (i < 0) return -i - 1;
        if (i >= size) return i = 2*size - i - 1;
        return i;
    }
}
