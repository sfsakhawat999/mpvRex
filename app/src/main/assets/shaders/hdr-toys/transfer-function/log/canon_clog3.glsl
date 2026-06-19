//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Canon CLog3)

float canon_clog3(float x) {
    const float sp0 = 0.014;
    const float c0 = 0.36726845;
    const float c1 = 14.98325;
    const float c2 = 0.12783901;
    const float c3 = 1.9754798;
    const float c4 = 0.12512219;
    const float c5 = 0.12240537;

    x /= 0.9;
    if (x < -sp0) {
        return -c0 * log(1.0 - c1 * x) / log(10.0) + c2;
    }
    if (x <= sp0) {
        return c3 * x + c4;
    }
    return c0 * log(c1 * x + 1.0) / log(10.0) + c5;
}

vec3 canon_clog3(vec3 color) {
    return vec3(
        canon_clog3(color.r),
        canon_clog3(color.g),
        canon_clog3(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = canon_clog3(color.rgb);

    return color;
}
