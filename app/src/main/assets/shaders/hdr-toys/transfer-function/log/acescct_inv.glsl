//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (ACEScct, inverse)

float acescct_inv(float x) {
    const float cut2 = 0.155251141552511;
    const float A = 10.5402377416545;
    const float B = 0.0729055341958355;
    const float C = 9.72;
    const float D = 17.52;

    if (x <= cut2) {
        return (x - B) / A;
    }
    return exp2(x * D - C);
}

vec3 acescct_inv(vec3 color) {
    return vec3(
        acescct_inv(color.r),
        acescct_inv(color.g),
        acescct_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = acescct_inv(color.rgb);

    return color;
}
