//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Panasonic V-Log, inverse)

float panasonic_vlog_inv(float x) {
    const float cut2 = 0.181;
    const float b = 0.00873;
    const float c = 0.241514;
    const float d = 0.598206;

    if (x < cut2) {
        return (x - 0.125) / 5.6;
    }
    return pow(10.0, (x - d) / c) - b;
}

vec3 panasonic_vlog_inv(vec3 color) {
    return vec3(
        panasonic_vlog_inv(color.r),
        panasonic_vlog_inv(color.g),
        panasonic_vlog_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = panasonic_vlog_inv(color.rgb);

    return color;
}
