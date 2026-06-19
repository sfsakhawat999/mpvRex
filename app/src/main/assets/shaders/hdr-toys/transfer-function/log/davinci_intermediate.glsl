//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (DaVinci Intermediate)

float davinci_intermediate(float x) {
    const float A = 0.0075;
    const float B = 7.0;
    const float C = 0.07329248;
    const float M = 10.44426855;
    const float LIN_CUT = 0.00262409;

    if (x <= LIN_CUT) {
        return x * M;
    }
    return (log2(x + A) + B) * C;
}

vec3 davinci_intermediate(vec3 color) {
    return vec3(
        davinci_intermediate(color.r),
        davinci_intermediate(color.g),
        davinci_intermediate(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = davinci_intermediate(color.rgb);

    return color;
}
