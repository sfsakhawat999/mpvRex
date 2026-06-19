//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (ACEScc)

float acescc(float x) {
    const float A = 9.72;
    const float B = 17.52;

    if (x <= 0.0) {
        return (log2(pow(2.0, -16.0)) + A) / B;
    }
    if (x < pow(2.0, -15.0)) {
        return (log2(pow(2.0, -16.0) + x / 2.0) + A) / B;
    }
    return (log2(x) + A) / B;
}

vec3 acescc(vec3 color) {
    return vec3(
        acescc(color.r),
        acescc(color.g),
        acescc(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = acescc(color.rgb);

    return color;
}
