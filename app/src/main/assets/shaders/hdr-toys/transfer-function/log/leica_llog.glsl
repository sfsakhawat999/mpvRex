//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Leica L-Log)

float leica_llog(float x) {
    const float a = 8.0;
    const float b = 0.09;
    const float c = 0.27;
    const float d = 1.3;
    const float e = 0.0115;
    const float f = 0.6;
    const float c0 = 0.006;

    if (x < c0) {
        return a * x + b;
    }
    return c * log(d * x + e) / log(10.0) + f;
}

vec3 leica_llog(vec3 color) {
    return vec3(
        leica_llog(color.r),
        leica_llog(color.g),
        leica_llog(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = leica_llog(color.rgb);

    return color;
}
