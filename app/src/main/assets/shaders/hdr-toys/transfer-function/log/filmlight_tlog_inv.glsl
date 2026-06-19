//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Filmlight T-Log, inverse)

float filmlight_tlog_inv(float x) {
    const float o = 0.075;
    const float A = 0.5520126568606655;
    const float B = 0.09232902596577353;
    const float C = 0.0057048244042473785;
    const float G = 16.184376489665897;

    if (x < o) {
        return (x - o) / G;
    }
    return exp((x - A) / B) - C;
}

vec3 filmlight_tlog_inv(vec3 color) {
    return vec3(
        filmlight_tlog_inv(color.r),
        filmlight_tlog_inv(color.g),
        filmlight_tlog_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = filmlight_tlog_inv(color.rgb);

    return color;
}
