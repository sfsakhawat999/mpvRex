//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (DJI D-Log)

float dji_dlog(float x) {
    if (x <= 0.0078) {
        return 6.025 * x + 0.0929;
    }
    return log(x * 0.9892 + 0.0108) / log(10.0) * 0.256663 + 0.584555;
}

vec3 dji_dlog(vec3 color) {
    return vec3(
        dji_dlog(color.r),
        dji_dlog(color.g),
        dji_dlog(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = dji_dlog(color.rgb);

    return color;
}
