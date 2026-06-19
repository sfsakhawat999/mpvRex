//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (DJI D-Log, inverse)

float dji_dlog_inv(float x) {
    if (x <= 0.14) {
        return (x - 0.0929) / 6.025;
    }
    return (pow(10.0, 3.89616 * x - 2.27752) - 0.0108) / 0.9892;
}

vec3 dji_dlog_inv(vec3 color) {
    return vec3(
        dji_dlog_inv(color.r),
        dji_dlog_inv(color.g),
        dji_dlog_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = dji_dlog_inv(color.rgb);

    return color;
}
