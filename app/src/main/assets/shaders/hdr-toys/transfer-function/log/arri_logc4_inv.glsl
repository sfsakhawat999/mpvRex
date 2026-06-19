//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Arri LogC4, inverse)

float arri_logc4_inv(float x) {
    const float a = (exp2(18.0) - 16.0) / 117.45;
    const float b = (1023.0 - 95.0) / 1023.0;
    const float c = 95.0 / 1023.0;
    const float s = (7.0 * log(2.0) * exp2(7.0 - 14.0 * c / b)) / (a * b);
    const float t = (exp2(14.0 * (-c / b) + 6.0) - 64.0) / a;

    if (x < t) {
        return x * s + t;
    }
    return (exp2(14.0 * (x - c) / b + 6.0) - 64.0) / a;
}

vec3 arri_logc4_inv(vec3 color) {
    return vec3(
        arri_logc4_inv(color.r),
        arri_logc4_inv(color.g),
        arri_logc4_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = arri_logc4_inv(color.rgb);

    return color;
}
