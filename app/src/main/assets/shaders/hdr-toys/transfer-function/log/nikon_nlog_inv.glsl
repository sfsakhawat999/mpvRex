//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Nikon N-Log, inverse)

float nikon_nlog_inv(float x) {
    const float a = 619.0 / 1023.0;
    const float b = 150.0 / 1023.0;
    const float c = 650.0 / 1023.0;
    const float d = 0.0075;
    const float c0 = 452.0 / 1023.0;

    if (x > c0) {
        return exp((x - a) / b);
    }
    return pow(x / c, 3.0) - d;
}

vec3 nikon_nlog_inv(vec3 color) {
    return vec3(
        nikon_nlog_inv(color.r),
        nikon_nlog_inv(color.g),
        nikon_nlog_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = nikon_nlog_inv(color.rgb);

    return color;
}
