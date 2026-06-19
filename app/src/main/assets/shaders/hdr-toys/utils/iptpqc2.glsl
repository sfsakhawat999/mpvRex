//!HOOK MAIN
//!BIND HOOKED
//!DESC IPTPQc2

// For vo=gpu, this fixes wrong color with Dolby Vision Profile 5,
// caused by IPTPQc2 being treated as YCbCr.
// vo=gpu-next doesn't needed this, so for test only.

// Add this to mpv.conf:
// vf=format:dolbyvision=no:colormatrix=bt.2020-ncl:primaries=bt.2020:transfer=pq

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
         0.4002,  0.7076, -0.0808,
        -0.2263,  1.1653,  0.0457,
         0.0000,  0.0000,  0.9182
    );
}

vec3 LMS_to_XYZ(vec3 LMS) {
    return LMS * inverse(mat3(
         0.4002,  0.7076, -0.0808,
        -0.2263,  1.1653,  0.0457,
         0.0000,  0.0000,  0.9182
    ));
}

vec3 LMS_to_IPT(vec3 LMS) {
    return LMS * mat3(
        0.4000,  0.4000,  0.2000,
        4.4550, -4.8510,  0.3960,
        0.8056,  0.3572, -1.1628
    );
}

vec3 IPT_to_LMS(vec3 IPT) {
    return IPT * inverse(mat3(
        0.4000,  0.4000,  0.2000,
        4.4550, -4.8510,  0.3960,
        0.8056,  0.3572, -1.1628
    ));
}

vec3 crosstalk(vec3 x, float a) {
    float b = 1.0 - 2.0 * a;
    mat3 transform = mat3(
        b, a, a,
        a, b, a,
        a, a, b
    );
    return x * transform;
}

vec3 crosstalk_inv(vec3 x, float a) {
    float b = 1.0 - a;
    float c = 1.0 / (1.0 - 3.0 * a);
    mat3 transform = mat3(
        b, -a, -a,
        -a, b, -a,
        -a, -a, b
    );
    return x * transform * c;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_YCbCr(color.rgb);
    color.rgb = IPT_to_LMS(color.rgb);
    color.rgb = max(color.rgb, 0.0);
    color.rgb = pq_eotf(color.rgb);
    color.rgb = crosstalk_inv(color.rgb, 0.02);
    color.rgb = LMS_to_XYZ(color.rgb);
    color.rgb = XYZ_to_RGB(color.rgb);
    color.rgb = max(color.rgb, 0.0);
    color.rgb = pq_eotf_inv(color.rgb);

    return color;
}
