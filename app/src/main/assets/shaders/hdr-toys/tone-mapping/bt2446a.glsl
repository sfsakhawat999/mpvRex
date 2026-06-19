// ITU-R BT.2446 Conversion Method A
// https://www.itu.int/pub/R-REP-BT.2446

//!PARAM max_luma
//!TYPE float
0.0

//!PARAM max_cll
//!TYPE float
0.0

//!PARAM reference_white
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1000.0
203.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC tone mapping (bt.2446a)

const vec3 y_coef = vec3(0.2627002120112671, 0.6779980715188708, 0.05930171646986196);

const float a = y_coef.r;
const float b = y_coef.g;
const float c = y_coef.b;
const float d = 2.0 * (1.0 - c);
const float e = 2.0 * (1.0 - a);

vec3 RGB_to_YCbCr(vec3 RGB) {
    return RGB * mat3(
         a,      b,      c,
        -a / d, -b / d,  0.5,
         0.5,   -b / e, -c / e
    );
}

vec3 YCbCr_to_RGB(vec3 YCbCr) {
    return YCbCr * mat3(
        1.0,  0.0,        e,
        1.0, -c / b * d, -a / b * e,
        1.0,  d,          0.0
    );
}

float get_max_l() {
    if (max_cll > 0.0)
        return max_cll;

    if (max_luma > 0.0)
        return max_luma;

    return 1000.0;
}

float f(float Y) {
    Y = pow(Y, 1.0 / 2.4);

    float pHDR = 1.0 + 32.0 * pow(get_max_l() / 10000.0, 1.0 / 2.4);
    float pSDR = 1.0 + 32.0 * pow(reference_white / 10000.0, 1.0 / 2.4);

    float Yp = log(1.0 + (pHDR - 1.0) * Y) / log(pHDR);

    float Yc;
    if      (Yp <= 0.7399)  Yc = Yp * 1.0770;
    else if (Yp <  0.9909)  Yc = Yp * (-1.1510 * Yp + 2.7811) - 0.6302;
    else                    Yc = Yp * 0.5000 + 0.5000;

    float Ysdr = (pow(pSDR, Yc) - 1.0) / (pSDR - 1.0);

    Y = pow(Ysdr, 2.4);

    return Y;
}

float curve(float Y) {
    return f(Y);
}

vec3 tone_mapping(vec3 YCbCr) {
    YCbCr /= get_max_l() / reference_white;

    float Y  = YCbCr.r;
    float Cb = YCbCr.g;
    float Cr = YCbCr.b;

    float Ysdr = curve(Y);

    float Yr = Ysdr / max(1.1 * Y, 1e-6);
    Cb *= Yr;
    Cr *= Yr;
    Y = Ysdr - max(0.1 * Cr, 0.0);

    return vec3(Y, Cb, Cr);
}

float f_slope(float x0, float y0, float x1, float y1) {
    float num = (y1 - y0);
    float den = (x1 - x0);
    return abs(den) < 1e-6 ? 1.0 : num / den;
}

float f_intercept(float slope, float x0, float y0) {
    return y0 - slope * x0;
}

float f_linear(float x, float slope, float intercept) {
    return slope * x + intercept;
}

float f_scale(float x, float x0, float y0, float x1, float y1) {
    float slope = f_slope(x0, y0, x1, y1);
    float intercept = f_intercept(slope, x0, y0);
    return f_linear(x, slope, intercept);
}

vec3 f_scale(vec3 x, float x0, float y0, float x1, float y1) {
    return vec3(
        f_scale(x.x, x0, y0, x1, y1),
        f_scale(x.y, x0, y0, x1, y1),
        f_scale(x.z, x0, y0, x1, y1)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_YCbCr(color.rgb);
    color.rgb = tone_mapping(color.rgb);
    color.rgb = YCbCr_to_RGB(color.rgb);
    color.rgb = f_scale(color.rgb, 0.0, 0.001, 1.0, 1.0);

    return color;
}
