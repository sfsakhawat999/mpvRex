//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (ACEScct)

float acescct(float x) {
    const float cut1 = 0.0078125;
    const float A = 10.5402377416545;
    const float B = 0.0729055341958355;
    const float C = 9.72;
    const float D = 17.52;

    if (x <= cut1) {
        return A * x + B;
    }
    return (log2(x) + C) / D;
}

vec3 acescct(vec3 color) {
    return vec3(
        acescct(color.r),
        acescct(color.g),
        acescct(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = acescct(color.rgb);

    return color;
}
