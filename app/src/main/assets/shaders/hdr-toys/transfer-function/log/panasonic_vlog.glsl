//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Panasonic V-Log)

float panasonic_vlog(float x) {
    const float cut1 = 0.01;
    const float b = 0.00873;
    const float c = 0.241514;
    const float d = 0.598206;

    if (x < cut1) {
        return 5.6 * x + 0.125;
    }
    return c * log(x + b) / log(10.0) + d;
}

vec3 panasonic_vlog(vec3 color) {
    return vec3(
        panasonic_vlog(color.r),
        panasonic_vlog(color.g),
        panasonic_vlog(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = panasonic_vlog(color.rgb);

    return color;
}
