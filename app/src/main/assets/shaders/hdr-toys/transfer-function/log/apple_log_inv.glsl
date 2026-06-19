//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Apple Log, inverse)

float apple_log_inv(float x) {
    const float R0 = -0.05641088;
    const float Rt = 0.01;
    const float c  = 47.28711236;
    const float b  = 0.00964052;
    const float y  = 0.08550479;
    const float d  = 0.69336945;

    const float Pt = c * pow(Rt - R0, 2.0);

    if (x < 0) {
        return R0;
    }

    if (x >= Pt) {
        return exp2((x - d) / y) - b;
    }

    return sqrt(x / c) + R0;
}

vec3 apple_log_inv(vec3 color) {
    return vec3(
        apple_log_inv(color.r),
        apple_log_inv(color.g),
        apple_log_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = apple_log_inv(color.rgb);

    return color;
}
