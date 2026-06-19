//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Sony S-Log2, inverse)

float sony_slog2_inv(float x) {
    const float c0 = 0.432699;
    const float c1 = 155.0;
    const float c2 = 219.0;
    const float c3 = 0.037584;
    const float c4 = 0.616596;
    const float c5 = 0.03;
    const float c6 = 3.53881278538813;
    const float c7 = 0.030001222851889303;

    float y = (x - 64.0 / 1023.0) / (876.0 / 1023.0);
    y = y < c7 ? (y - c7) / c6 : c2 * (pow(10.0, (y - c4 - c5) / c0) - c3) / c1;
    return y * 0.9;
}

vec3 sony_slog2_inv(vec3 color) {
    return vec3(
        sony_slog2_inv(color.r),
        sony_slog2_inv(color.g),
        sony_slog2_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = sony_slog2_inv(color.rgb);

    return color;
}
