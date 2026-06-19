//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Filmlight T-Log)

float filmlight_tlog(float x) {
    const float o = 0.075;
    const float A = 0.5520126568606655;
    const float B = 0.09232902596577353;
    const float C = 0.0057048244042473785;
    const float G = 16.184376489665897;

    if (x < 0.0) {
        return G * x + o;
    }
    return log(x + C) * B + A;
}

vec3 filmlight_tlog(vec3 color) {
    return vec3(
        filmlight_tlog(color.r),
        filmlight_tlog(color.g),
        filmlight_tlog(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = filmlight_tlog(color.rgb);

    return color;
}
