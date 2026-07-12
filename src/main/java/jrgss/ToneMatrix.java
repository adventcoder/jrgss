package jrgss;

//
// a = 1 - 2/3 T.g
// b = 1/3 T.g
// [r'] = [a b b T.r] [r]
// [g']   [b a b T.g] [g]
// [b']   [b b a T.b] [b]
// [1 ]   [0 0 0   1] [1]
//
//
// T.apply(rgb) = rgb (1-T.g) + avg(rgb) T.g + T.rgb
//
// T1.apply(T2.apply(rgb)) = T3.apply(rgb)
//
// T3.g = T1.g + T2.g - T1.g T2.g
// T3.rgb = T1.apply(T2.rgb)
//
public class ToneMatrix {
    public final float[] rgb = new float[3];
    public float g;

    public void apply(float[] src, float[] dst) {
        float avg = (src[0] + src[1] + src[2]) / 3;
        dst[0] = (1-g)*src[0] + avg*g + rgb[0];
        dst[1] = (1-g)*src[1] + avg*g + rgb[1];
        dst[2] = (1-g)*src[2] + avg*g + rgb[2];
    }

    public void apply(ToneMatrix src, ToneMatrix dst) {
        apply(src.rgb, dst.rgb);
        dst.g = g + src.g - g*src.g;
    }
}
