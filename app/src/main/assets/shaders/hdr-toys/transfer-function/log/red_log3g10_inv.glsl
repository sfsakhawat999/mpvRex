//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Red Log3G10, inverse)

float red_log3g10_inv(float x) {
    const float a = 0.224282;
    const float b = 155.975327;
    const float c = 0.01;
    const float g = 15.1927;

    if (x < 0.0) {
        return x / g - c;
    }
    return (pow(10.0, x / a) - 1.0) / b - c;
}

vec3 red_log3g10_inv(vec3 color) {
    return vec3(
        red_log3g10_inv(color.r),
        red_log3g10_inv(color.g),
        red_log3g10_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = red_log3g10_inv(color.rgb);

    return color;
}
