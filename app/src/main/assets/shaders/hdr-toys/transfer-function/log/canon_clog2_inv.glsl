//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Canon CLog2, inverse)

float canon_clog2_inv(float x) {
    const float c0 = 0.092864125;
    const float c1 = 0.24136077;
    const float c2 = 87.099375;

    float y;
    if (x < c0) {
        y = -(pow(10.0, (c0 - x) / c1) - 1.0) / c2;
    } else {
        y = (pow(10.0, (x - c0) / c1) - 1.0) / c2;
    }
    return y * 0.9;
}

vec3 canon_clog2_inv(vec3 color) {
    return vec3(
        canon_clog2_inv(color.r),
        canon_clog2_inv(color.g),
        canon_clog2_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = canon_clog2_inv(color.rgb);

    return color;
}
