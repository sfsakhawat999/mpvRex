//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (DaVinci Intermediate, inverse)

float davinci_intermediate_inv(float x) {
    const float A = 0.0075;
    const float B = 7.0;
    const float C = 0.07329248;
    const float M = 10.44426855;
    const float LOG_CUT = 0.02740668;

    if (x <= LOG_CUT) {
        return x / M;
    }
    return exp2(x / C - B) - A;
}

vec3 davinci_intermediate_inv(vec3 color) {
    return vec3(
        davinci_intermediate_inv(color.r),
        davinci_intermediate_inv(color.g),
        davinci_intermediate_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = davinci_intermediate_inv(color.rgb);

    return color;
}
