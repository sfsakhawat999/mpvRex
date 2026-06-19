//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Red Log3G10)

float red_log3g10(float x) {
    const float a = 0.224282;
    const float b = 155.975327;
    const float c = 0.01;
    const float g = 15.1927;

    if (x < -c) {
        return (x + c) * g;
    }
    return a * log((x + c) * b + 1.0) / log(10.0);
}

vec3 red_log3g10(vec3 color) {
    return vec3(
        red_log3g10(color.r),
        red_log3g10(color.g),
        red_log3g10(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = red_log3g10(color.rgb);

    return color;
}
