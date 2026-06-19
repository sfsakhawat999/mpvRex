//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Leica L-Log, inverse)

float leica_llog_inv(float x) {
    const float a = 8.0;
    const float b = 0.09;
    const float c = 0.27;
    const float d = 1.3;
    const float e = 0.0115;
    const float f = 0.6;
    const float c1 = 0.138;

    if (x < c1) {
        return (x - b) / a;
    }
    return (pow(10.0, (x - f) / c) - e) / d;
}

vec3 leica_llog_inv(vec3 color) {
    return vec3(
        leica_llog_inv(color.r),
        leica_llog_inv(color.g),
        leica_llog_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = leica_llog_inv(color.rgb);

    return color;
}
