//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Sony S-Log3)

float sony_slog3(float x) {
    const float a = 0.01125;
    const float b = 420.0;
    const float c = 261.5;
    const float d = 171.2102946929;
    const float e = 95.0;
    const float f = 0.18;
    const float o = 0.01;

    if (x < a) {
        return (x * (d - e) / a + e) / 1023.0;
    }
    return (b + log((x + o) / (f + o)) / log(10.0) * c) / 1023.0;
}

vec3 sony_slog3(vec3 color) {
    return vec3(
        sony_slog3(color.r),
        sony_slog3(color.g),
        sony_slog3(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = sony_slog3(color.rgb);

    return color;
}
