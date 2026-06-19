//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Apple Log)

float apple_log(float x) {
    const float R0 = -0.05641088;
    const float Rt = 0.01;
    const float c  = 47.28711236;
    const float b  = 0.00964052;
    const float y  = 0.08550479;
    const float d  = 0.69336945;

    if (x < R0) {
        return 0.0;
    }

    if (x >= Rt) {
        return y * log2(x + b) + d;
    }

    return c * (pow(x - R0, 2.0));
}

vec3 apple_log(vec3 color) {
    return vec3(
        apple_log(color.r),
        apple_log(color.g),
        apple_log(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = apple_log(color.rgb);

    return color;
}
