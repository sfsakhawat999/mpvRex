// https://ieeexplore.ieee.org/document/7291452
// https://www.itu.int/rec/R-REC-BT.2100

// https://www.itu.int/pub/R-REP-BT.2390
// pq ootf: 100.0 * bt1886_eotf(bt709_oetf(59.5208 * x), 2.4, 1.0, 0.0)

//!PARAM reference_white
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1000.0
203.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (pq)

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

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = pq_eotf_inv(color.rgb * reference_white);

    return color;
}
