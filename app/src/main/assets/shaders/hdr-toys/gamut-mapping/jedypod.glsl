//!PARAM cyan_limit
//!TYPE float
//!MINIMUM 1.01
//!MAXIMUM 2.0
1.595

//!PARAM magenta_limit
//!TYPE float
//!MINIMUM 1.01
//!MAXIMUM 2.0
1.089

//!PARAM yellow_limit
//!TYPE float
//!MINIMUM 1.01
//!MAXIMUM 2.0
1.117

//!PARAM cyan_threshold
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.990

//!PARAM magenta_threshold
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.940

//!PARAM yellow_threshold
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.977

//!PARAM bleach_falloff
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 5.0
2.5

//!PARAM softness_scale
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.25

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC gamut mapping (jedypod)

float max3(vec3 v) {
  return max(max(v.x, v.y), v.z);
}

float min3(vec3 v) {
  return min(min(v.x, v.y), v.z);
}

// https://www.colour-science.org:8010/apps/rgb_colourspace_transformation_matrix
vec3 bt2020_to_bt709(vec3 color) {
    const mat3 m_bt2020_to_bt709 = mat3(
         1.6604910021, -0.5876411388, -0.0728498633,
        -0.1245504745,  1.1328998971, -0.0083494226,
        -0.0181507634, -0.1005788980,  1.1187296614
    );
    return color * m_bt2020_to_bt709;
}

// https://github.com/aces-aswf/aces-look/blob/main/reference_gamut_compression/Look.Academy.ReferenceGamutCompress.ctl
float compress_power(float x, float t, float x0, float y0) {
    float pwr = 1.2;

    if (x < t) {
        return x; // No compression below threshold
    }

    // Calculate scale factor for y = y0 intersect
    float scl = (x0 - t) / pow(pow((y0 - t) / (x0 - t), -pwr) - 1.0, 1.0 / pwr);

    // Normalize distance outside threshold by scale factor
    float nd = (x - t) / scl;
    float p = pow(nd, pwr);

    return t + scl * nd / (pow(1.0 + p, 1.0 / pwr));
}

// parabolic compression function
// https://www.desmos.com/calculator/khowxlu6xh
float compress_parabolic(float x, float t, float x0, float y0) {
    float s = (y0 - t) / sqrt(max(x0 - y0, 1e-6));
    float ox = t - s * s / 4.0;
    float oy = t - s * sqrt(s * s / 4.0);
    return (x < t ? x : s * sqrt(x - ox) + oy);
}

// https://github.com/jedypod/gamut-compress
vec3 gamut_compress_jedypod(vec3 rgb) {
    // Achromatic axis
    float ac = max3(rgb);

    // Inverse RGB Ratios: distance from achromatic axis
    vec3 d = ac == 0.0 ? vec3(0.0) : (ac - rgb) / abs(ac);

    // Compressed distance
    vec3 cd = vec3(
        compress_parabolic(d.x, cyan_threshold, cyan_limit, 1.0),
        compress_parabolic(d.y, magenta_threshold, magenta_limit, 1.0),
        compress_parabolic(d.z, yellow_threshold, yellow_limit, 1.0)
    );

    // Inverse RGB Ratios to RGB
    vec3 crgb = ac - cd * abs(ac);

    return crgb;
}

// Ronja's HSV to RGB conversion
// https://www.ronja-tutorials.com/post/041-hsv-colorspace/
vec3 hue_to_rgb(float hue) {
    hue = fract(hue);
    float r = abs(hue * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(hue * 6.0 - 2.0);
    float b = 2.0 - abs(hue * 6.0 - 4.0);
    return clamp(vec3(r, g, b), 0.0, 1.0);
}

vec3 hsv_to_rgb(vec3 hsv) {
    return hsv.z * mix(vec3(1.0), hue_to_rgb(hsv.x), hsv.y);
}

vec3 rgb_to_hsv(vec3 rgb) {
    float max_c = max3(rgb);
    float min_c = min3(rgb);
    float diff = max_c - min_c;

    float hue = 0.0;
    float saturation = 0.0;
    float value = max_c;

    if (diff > 0.0 && value > 0.0) {
        float diff_inv = 1.0 / diff;
        if (value == rgb.r)      hue = (rgb.g - rgb.b) * diff_inv;
        else if (value == rgb.g) hue = 2.0 + (rgb.b - rgb.r) * diff_inv;
        else                     hue = 4.0 + (rgb.r - rgb.g) * diff_inv;
        hue = fract(hue / 6.0);
        saturation = diff / value;
    }

    return vec3(hue, saturation, value);
}


// Make the colors that still overflow tend toward white.
// https://www.youtube.com/watch?v=6h4mpYdQQMk
float bleach_strength(float brightness) {
    return (1.0 - exp(-brightness)) * exp(-bleach_falloff);
}

vec3 gamut_compress_bleach(vec3 rgb) {
    vec3 rgb_overflow = max(rgb - 1.0, vec3(0.0));
    vec3 hsv_overflow = rgb_to_hsv(rgb_overflow);
    vec3 hsv_invert = vec3(fract(hsv_overflow.x + 0.5), hsv_overflow.yz);
    vec3 rgb_invert = hsv_to_rgb(hsv_invert);
    vec3 rgb_bleach = rgb + rgb_invert * bleach_strength(hsv_invert.z);
    return clamp(rgb_bleach, vec3(0.0), vec3(1.0));
}

// Taken from Björn Ottosson's gamut compression
// https://www.shadertoy.com/view/7sXcWn
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

    color.rgb = bt2020_to_bt709(color.rgb);
    color.rgb = gamut_compress_jedypod(color.rgb);
    color.rgb = gamut_compress_bleach(color.rgb);
    color.rgb = soft_clip(color.rgb);

    return color;
}
