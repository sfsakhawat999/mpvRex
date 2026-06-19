//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Canon CLog3, inverse)

float canon_clog3_inv(float x) {
    const float sp1 = 0.09746547;
    const float sp2 = 0.15277891;
    const float c0 = 0.36726845;
    const float c1 = 14.98325;
    const float c2 = 0.12783901;
    const float c3 = 1.9754798;
    const float c4 = 0.12512219;
    const float c5 = 0.12240537;

    float y;
    if (x < sp1) {
        y = -(pow(10.0, (c2 - x) / c0) - 1.0) / c1;
    } else if (x <= sp2) {
        y = (x - c4) / c3;
    } else {
        y = (pow(10.0, (x - c5) / c0) - 1.0) / c1;
    }
    return y * 0.9;
}

vec3 canon_clog3_inv(vec3 color) {
    return vec3(
        canon_clog3_inv(color.r),
        canon_clog3_inv(color.g),
        canon_clog3_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = canon_clog3_inv(color.rgb);

    return color;
}
