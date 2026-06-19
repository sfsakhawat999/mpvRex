//!PARAM softness_scale
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.5

//!PARAM highlight_extend
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.09

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC gamut mapping (bottosson)

float max3(vec3 v) {
  return max(max(v.x, v.y), v.z);
}

float min3(vec3 v) {
  return min(min(v.x, v.y), v.z);
}

float cbrt(float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

vec3 cbrt(vec3 color) {
    return vec3(
        cbrt(color.x),
        cbrt(color.y),
        cbrt(color.z)
    );
}

// BT.2020
vec3 RGB_to_XYZ(vec3 RGB) {
    return RGB * mat3(
        0.6369580483012914, 0.14461690358620832,  0.1688809751641721,
        0.2627002120112671, 0.6779980715188708,   0.05930171646986196,
        0.000000000000000,  0.028072693049087428, 1.060985057710791
    );
}

vec3 XYZ_to_RGB(vec3 XYZ) {
    return XYZ * mat3(
         1.716651187971268,  -0.355670783776392, -0.253366281373660,
        -0.666684351832489,   1.616481236634939,  0.0157685458139111,
         0.017639857445311,  -0.042770613257809,  0.942103121235474
    );
}

// BT.709
vec3 RGB_to_XYZ_output(vec3 RGB) {
    mat3 M = mat3(
        0.41239079926595934, 0.357584339383878,   0.1804807884018343,
        0.21263900587151027, 0.715168678767756,   0.07219231536073371,
        0.01933081871559182, 0.11919477979462598, 0.9505321522496607);
    return RGB * M;
}

vec3 XYZ_to_RGB_output(vec3 XYZ) {
    mat3 M = mat3(
        3.2409699419045226,  -1.537383177570094,   -0.4986107602930034,
        -0.9692436362808796,   1.8759675015077202,   0.04155505740717559,
        0.05563007969699366, -0.20397695888897652,  1.0569715142428786);
    return XYZ * M;
}

vec3 XYZ_to_LMS(vec3 XYZ) {
    return XYZ * mat3(
        0.8190224379967030, 0.3619062600528904, -0.1288737815209879,
        0.0329836539323885, 0.9292868615863434,  0.0361446663506424,
        0.0481771893596242, 0.2642395317527308,  0.6335478284694309
    );
}

vec3 LMS_to_XYZ(vec3 LMS) {
    return LMS * mat3(
         1.2268798758459243, -0.5578149944602171,  0.2813910456659647,
        -0.0405757452148008,  1.1122868032803170, -0.0717110580655164,
        -0.0763729366746601, -0.4214933324022432,  1.5869240198367816
    );
}

vec3 LMS_to_Lab(vec3 LMS) {
    return LMS * mat3(
        0.2104542683093140,  0.7936177747023054, -0.0040720430116193,
        1.9779985324311684, -2.4285922420485799,  0.4505937096174110,
        0.0259040424655478,  0.7827717124575296, -0.8086757549230774
    );
}

vec3 Lab_to_LMS(vec3 Lab) {
    return Lab * mat3(
        1.0000000000000000,  0.3963377773761749,  0.2158037573099136,
        1.0000000000000000, -0.1055613458156586, -0.0638541728258133,
        1.0000000000000000, -0.0894841775298119, -1.2914855480194092
    );
}

vec3 RGB_to_Lab(vec3 color) {
    color = RGB_to_XYZ(color);
    color = XYZ_to_LMS(color);
    color = cbrt(color);
    color = LMS_to_Lab(color);
    return color;
}

vec3 Lab_to_RGB(vec3 color) {
    color = Lab_to_LMS(color);
    color = pow(color, vec3(3.0));
    color = LMS_to_XYZ(color);
    color = XYZ_to_RGB(color);
    return color;
}

const float epsilon = 1e-6;

vec3 Lab_to_LCh(vec3 Lab) {
    float L = Lab.x;
    float a = Lab.y;
    float b = Lab.z;

    float C = length(vec2(a, b));
    float h = (abs(a) < epsilon && abs(b) < epsilon) ? 0.0 : atan(b, a);

    return vec3(L, C, h);
}

vec3 LCh_to_Lab(vec3 LCh) {
    float L = LCh.x;
    float C = LCh.y;
    float h = LCh.z;

    C = max(C, 0.0);
    float a = C * cos(h);
    float b = C * sin(h);

    return vec3(L, a, b);
}

// Gamut compression
// by Björn Ottosson
// https://www.shadertoy.com/view/7sXcWn

vec2 findCenterAndPurity(vec3 x) {
    // Matrix derived for (c_smooth+s_smooth) to be an approximation of the macadam limit
    // this makes it some kind of g0-like estimate
    mat3 M = mat3(
         2.26775149, -1.43293879,  0.1651873,
        -0.98535505,  2.1260072,  -0.14065215,
        -0.02501605, -0.26349465,  1.2885107
    );

    x = x * M;

    float x_min = min(x.r, min(x.g, x.b));
    float x_max = max(x.r, max(x.g, x.b));

    float c = (x_max + x_min) * 0.5;
    float s = (x_max - x_min);

    // math trickery to create values close to c and s, but without producing hard edges
    vec3 y = (x - c) / s;
    float c_smooth = c + dot(y * y * y, vec3(1.0 / 3.0)) * s;
    float s_smooth = sqrt(dot(x - c, x - c) / 2.0);
    return vec2(c_smooth, s_smooth);
}

vec3 calculateLCh(vec3 c) {
    vec3 lms = cbrt(XYZ_to_LMS(RGB_to_XYZ_output(c)));

    float maxLms = findCenterAndPurity(lms).x;

    vec3 LCh = Lab_to_LCh(LMS_to_Lab(lms));

    return vec3(maxLms, LCh.y, LCh.z);
}

vec2 expandShape(vec3 rgb, vec2 ST) {
    vec3 LCh = calculateLCh(rgb);
    vec2 STnew = vec2(LCh.x / LCh.y, (1.0 - LCh.x) / LCh.y);
    STnew = (STnew + 3.0 * STnew * STnew * LCh.y);
    return vec2(min(ST.x, STnew.x), min(ST.y, STnew.y));
}

float expandScale(vec3 rgb, vec2 ST, float scale) {
    vec3 LCh = calculateLCh(rgb);
    float Cnew = (1.0 / ((ST.x / LCh.x) + (ST.y / (1.0 - LCh.x))));
    return max(LCh.y / Cnew, scale);
}

vec2 approximateShape() {
    float m = -softness_scale * 0.2;
    float s = 1.0 + (softness_scale * 0.2 + softness_scale * 0.8);

    vec2 ST = vec2(1000.0, 1000.0);
    ST = expandShape(m + s * vec3(1.0, 0.0, 0.0), ST);
    ST = expandShape(m + s * vec3(1.0, 1.0, 0.0), ST);
    ST = expandShape(m + s * vec3(0.0, 1.0, 0.0), ST);
    ST = expandShape(m + s * vec3(0.0, 1.0, 1.0), ST);
    ST = expandShape(m + s * vec3(0.0, 0.0, 1.0), ST);
    ST = expandShape(m + s * vec3(1.0, 0.0, 1.0), ST);

    float scale = 0.0;
    scale = expandScale(m + s * vec3(1.0, 0.0, 0.0), ST, scale);
    scale = expandScale(m + s * vec3(1.0, 1.0, 0.0), ST, scale);
    scale = expandScale(m + s * vec3(0.0, 1.0, 0.0), ST, scale);
    scale = expandScale(m + s * vec3(0.0, 1.0, 1.0), ST, scale);
    scale = expandScale(m + s * vec3(0.0, 0.0, 1.0), ST, scale);
    scale = expandScale(m + s * vec3(1.0, 0.0, 1.0), ST, scale);

    return ST / scale;
}

// Soft hyperbolic compression: identity for x << k, asymptotically approaches k.
float soft_compress(float x, float k) {
    return x / sqrt(x * x / (k * k) + 1.0);
}

// Gamut mapping in OKLCh space: L and h are kept strictly unchanged,
// only C is compressed via a smooth hyperbolic function.
// This is a simplification of the original compute() + fix_L approach:
//   - C_out/C_in is equivalent to the saturation scale factor
//   - scaling C directly avoids any lightness drift
vec3 gmaut_mapping(vec3 lch) {
    float L = lch.x;
    float C = lch.y;
    float h = lch.z;

    vec2 ST = approximateShape();
    float Sx = ST.x;
    float Sy = ST.y;

    // L_peak is where C_smooth is maximum; derivative is 0 there.
    float L_peak = Sx / (Sx + Sy);
    float C_peak  = 1.0 / (Sx / max(L_peak, 1e-6) + Sy / max(1.0 - L_peak, 1e-6));

    float C_smooth;
    if (L <= L_peak) {
        // Left side: original formula, unchanged.
        C_smooth = 1.0 / (Sx / max(L, 1e-6) + Sy / max(1.0 - L, 1e-6));
    } else {
        // Right side: cubic Hermite from (L_peak, C_peak, slope=0) to (1+highlight_extend, 0, slope=0).
        // With both endpoint slopes = 0, Hermite reduces to smoothstep shape:
        //   C_smooth = C_peak * (1 - 3t² + 2t³)  where t = (L - L_peak) / (1 + highlight_extend - L_peak)
        float width = 1.0 + highlight_extend - L_peak;
        float t = clamp((L - L_peak) / max(width, 1e-6), 0.0, 1.0);
        C_smooth = C_peak * (1.0 - t * t * (3.0 - 2.0 * t));
    }

    return vec3(L, soft_compress(C, max(C_smooth, 1e-6)), h);
}

vec3 soft_saturate(vec3 x, vec3 a) {
    a = 1.0 + clamp(a, 0.0, softness_scale);
    x = min(x, a);
    vec3 b = (a - 1.0) * sqrt(a / (2.0 - a));

    vec3 dxa = x - a;
    vec3 dxa2 = dxa * dxa;
    vec3 a2 = a * a;
    vec3 b2 = b * b;

    return 1.0 - (sqrt(dxa2 + b2) - b) / (sqrt(a2 + b2) - b);
}

vec3 soft_clip(vec3 color) {
    const float grey = 0.18;

    float max_rgb = max3(color);
    float min_rgb = min3(color);

    vec3 x = color - grey;

    vec3 xsgn = sign(x);
    vec3 xscale = 0.5 + xsgn * (0.5 - grey);
    x /= xscale;

    float softness0 = max_rgb / (1.0 + softness_scale) * softness_scale;
    float softness1 = (1.0 - min_rgb) / (1.0 + softness_scale) * softness_scale;

    vec3 softness = vec3(0.5) * (softness0 + softness1 + xsgn * (softness1 - softness0));

    return grey + xscale * xsgn * soft_saturate(abs(x), softness);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_Lab(color.rgb);
    color.rgb = Lab_to_LCh(color.rgb);
    color.rgb = gmaut_mapping(color.rgb);
    color.rgb = LCh_to_Lab(color.rgb);
    color.rgb = Lab_to_RGB(color.rgb);
    color.rgb = RGB_to_XYZ(color.rgb);
    color.rgb = XYZ_to_RGB_output(color.rgb);
    color.rgb = max(color.rgb, 0.0);
    color.rgb = soft_clip(color.rgb);

    return color;
}
