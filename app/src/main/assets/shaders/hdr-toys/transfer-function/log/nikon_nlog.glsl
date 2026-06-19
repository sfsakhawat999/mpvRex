//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Nikon N-Log)

float nikon_nlog(float x) {
    const float b = 150.0 / 1023.0;
    const float c = 650.0 / 1023.0;
    const float a = 619.0 / 1023.0;
    const float d = 0.0075;
    const float c1 = 0.328;

    if (x > c1) {
        return b * log(x) + a;
    }
    return c * pow(x + d, 1.0 / 3.0);
}

vec3 nikon_nlog(vec3 color) {
    return vec3(
        nikon_nlog(color.r),
        nikon_nlog(color.g),
        nikon_nlog(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = nikon_nlog(color.rgb);

    return color;
}
