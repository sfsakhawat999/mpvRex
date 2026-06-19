//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Sony S-Log2)

float sony_slog2(float x) {
    const float c0 = 0.432699;
    const float c1 = 155.0;
    const float c2 = 219.0;
    const float c3 = 0.037584;
    const float c4 = 0.616596;
    const float c5 = 0.03;
    const float c6 = 3.53881278538813;
    const float c7 = 0.030001222851889303;

    float y = x / 0.9;
    y = y < 0.0 ? y * c6 + c7 : (c0 * log(c1 * y / c2 + c3) / log(10.0) + c4) + c5;
    return y * (876.0 / 1023.0) + 64.0 / 1023.0;
}

vec3 sony_slog2(vec3 color) {
    return vec3(
        sony_slog2(color.r),
        sony_slog2(color.g),
        sony_slog2(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = sony_slog2(color.rgb);

    return color;
}
