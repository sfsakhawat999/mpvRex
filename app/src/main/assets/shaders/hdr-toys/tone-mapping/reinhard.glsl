// Photographic tone reproduction for digital images
// https://doi.org/10.1145/566654.566575

//!PARAM max_luma
//!TYPE float
0.0

//!PARAM max_cll
//!TYPE float
0.0

//!PARAM scene_max_r
//!TYPE float
0.0

//!PARAM scene_max_g
//!TYPE float
0.0

//!PARAM scene_max_b
//!TYPE float
0.0

//!PARAM max_pq_y
//!TYPE float
0.0

//!PARAM reference_white
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1000.0
203.0

//!PARAM chroma_correction_scaling
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
1.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC tone mapping (reinhard)

const float m1 = 2610.0 / 4096.0 / 4.0;
const float m2 = 2523.0 / 4096.0 * 128.0;
const float c1 = 3424.0 / 4096.0;
const float c2 = 2413.0 / 4096.0 * 32.0;
const float c3 = 2392.0 / 4096.0 * 32.0;
const float pw = 10000.0;

float pq_eotf_inv(float x) {
    float t = pow(x / pw, m1);
    return pow((c1 + c2 * t) / (1.0 + c3 * t), m2);
}

vec3 pq_eotf_inv(vec3 color) {
    return vec3(
        pq_eotf_inv(color.r),
        pq_eotf_inv(color.g),
        pq_eotf_inv(color.b)
    );
}

float pq_eotf(float x) {
    float t = pow(x, 1.0 / m2);
    return pow(max(t - c1, 0.0) / (c2 - c3 * t), 1.0 / m1) * pw;
}

vec3 pq_eotf(vec3 color) {
    return vec3(
        pq_eotf(color.r),
        pq_eotf(color.g),
        pq_eotf(color.b)
    );
}

vec3 RGB_to_XYZ(vec3 RGB) {
    return RGB * mat3(
        0.6369580483012914, 0.14461690358620832,  0.1688809751641721,
        0.2627002120112671, 0.6779980715188708,   0.05930171646986196,
        0.0               , 0.028072693049087428, 1.060985057710791
    );
}

vec3 XYZ_to_RGB(vec3 XYZ) {
    return XYZ * mat3(
         1.716651187971268, -0.355670783776392, -0.25336628137366,
        -0.666684351832489,  1.616481236634939,  0.0157685458139111,
         0.017639857445311, -0.042770613257809,  0.942103121235474
    );
}

vec3 XYZ_to_LMS(vec3 XYZ) {
    return XYZ * mat3(
         0.3592832590121217,  0.6976051147779502, -0.0358915932320290,
        -0.1920808463704993,  1.1004767970374321,  0.0753748658519118,
         0.0070797844607479,  0.0748396662186362,  0.8433265453898765
    );
}

vec3 LMS_to_XYZ(vec3 LMS) {
    return LMS * mat3(
         2.0701522183894223, -1.3263473389671563,  0.2066510476294053,
         0.3647385209748072,  0.6805660249472273, -0.0453045459220347,
        -0.0497472075358123, -0.0492609666966131,  1.1880659249923042
    );
}

vec3 LMS_to_ICtCp(vec3 LMS) {
    return LMS * mat3(
         2048.0 / 4096.0,   2048.0 / 4096.0,    0.0 / 4096.0,
         6610.0 / 4096.0, -13613.0 / 4096.0, 7003.0 / 4096.0,
        17933.0 / 4096.0, -17390.0 / 4096.0, -543.0 / 4096.0
    );
}

vec3 ICtCp_to_LMS(vec3 ICtCp) {
    return ICtCp * mat3(
        1.0,  0.0086090370379328,  0.1110296250030260,
        1.0, -0.0086090370379328, -0.1110296250030260,
        1.0,  0.5600313357106791, -0.3206271749873189
    );
}

vec3 RGB_to_ICtCp(vec3 color) {
    color *= reference_white;
    color = RGB_to_XYZ(color);
    color = XYZ_to_LMS(color);
    color = pq_eotf_inv(color);
    color = LMS_to_ICtCp(color);
    return color;
}

vec3 ICtCp_to_RGB(vec3 color) {
    color = ICtCp_to_LMS(color);
    color = pq_eotf(color);
    color = LMS_to_XYZ(color);
    color = XYZ_to_RGB(color);
    color /= reference_white;
    return color;
}

float get_max_l() {
    if (max_pq_y > 0.0)
        return pq_eotf(max_pq_y);

    if (scene_max_r > 0.0 || scene_max_g > 0.0 || scene_max_b > 0.0) {
        vec3 scene_max_rgb = vec3(scene_max_r, scene_max_g, scene_max_b);
        return RGB_to_XYZ(scene_max_rgb).y;
    }

    if (max_cll > 0.0)
        return max_cll;

    if (max_luma > 0.0)
        return max_luma;

    return 1000.0;
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

float f(float x, float w) {
    float simple = x / (1.0 + x);
    float extended = simple * (1.0 + x / (w * w));
    return extended;
}

float curve(float x) {
    float w = get_max_l();
    float y = f(x, w);
    float y_scaled = f_scale(y, 0.0, 0.001, 1.0, 1.0);
    return y_scaled;
}

vec2 chroma_correction(vec2 ab, float i1, float i2) {
    float r1 = i1 / max(i2, 1e-6);
    float r2 = i2 / max(i1, 1e-6);
    return ab * mix(1.0, min(r1, r2), chroma_correction_scaling);
}

vec3 tone_mapping(vec3 iab) {
    float i2 = pq_eotf_inv(curve(pq_eotf(iab.x) / reference_white) * reference_white);
    vec2 ab2 = chroma_correction(iab.yz, iab.x, i2);
    return vec3(i2, ab2);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_ICtCp(color.rgb);
    color.rgb = tone_mapping(color.rgb);
    color.rgb = ICtCp_to_RGB(color.rgb);

    return color;
}
