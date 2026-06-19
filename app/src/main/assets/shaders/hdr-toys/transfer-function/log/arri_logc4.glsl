//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Arri LogC4)

float arri_logc4(float x) {
    const float a = (exp2(18.0) - 16.0) / 117.45;
    const float b = (1023.0 - 95.0) / 1023.0;
    const float c = 95.0 / 1023.0;
    const float s = (7.0 * log(2.0) * exp2(7.0 - 14.0 * c / b)) / (a * b);
    const float t = (exp2(14.0 * (-c / b) + 6.0) - 64.0) / a;

    if (x < t) {
        return (x - t) / s;
    }
    return (log2(a * x + 64.0) - 6.0) / 14.0 * b + c;
}

vec3 arri_logc4(vec3 color) {
    return vec3(
        arri_logc4(color.r),
        arri_logc4(color.g),
        arri_logc4(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = arri_logc4(color.rgb);

    return color;
}
