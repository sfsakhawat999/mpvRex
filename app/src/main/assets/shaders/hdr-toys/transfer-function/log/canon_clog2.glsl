//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Canon CLog2)

float canon_clog2(float x) {
    const float c0 = 0.092864125;
    const float c1 = 0.24136077;
    const float c2 = 87.099375;

    x /= 0.9;
    if (x < 0.0) {
        return -c1 * log(1.0 - c2 * x) / log(10.0) + c0;
    }
    return c1 * log(c2 * x + 1.0) / log(10.0) + c0;
}

vec3 canon_clog2(vec3 color) {
    return vec3(
        canon_clog2(color.r),
        canon_clog2(color.g),
        canon_clog2(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = canon_clog2(color.rgb);

    return color;
}
