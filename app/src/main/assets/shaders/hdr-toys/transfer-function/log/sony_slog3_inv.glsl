//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Sony S-Log3, inverse)

float sony_slog3_inv(float x) {
    const float a = 0.01125;
    const float b = 420.0;
    const float c = 261.5;
    const float d = 171.2102946929;
    const float e = 95.0;
    const float f = 0.18;
    const float o = 0.01;

    if (x < d / 1023.0) {
        return (x * 1023.0 - e) * a / (d - e);
    }
    return pow(10.0, (x * 1023.0 - b) / c) * (f + o) - o;
}

vec3 sony_slog3_inv(vec3 color) {
    return vec3(
        sony_slog3_inv(color.r),
        sony_slog3_inv(color.g),
        sony_slog3_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = sony_slog3_inv(color.rgb);

    return color;
}
