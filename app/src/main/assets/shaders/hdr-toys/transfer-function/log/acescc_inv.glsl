//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (ACEScc, inverse)

float acescc_inv(float x) {
    const float A = 9.72;
    const float B = 17.52;

    if (x <= (A - 15.0) / B) {
        return (pow(2.0, x * B - A) - pow(2.0, -16.0)) * 2.0;
    }
    if (x < (log2(65504.0) + A) / B) {
        return pow(2.0, x * B - A);
    }
    return x;
}

vec3 acescc_inv(vec3 color) {
    return vec3(
        acescc_inv(color.r),
        acescc_inv(color.g),
        acescc_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = acescc_inv(color.rgb);

    return color;
}
