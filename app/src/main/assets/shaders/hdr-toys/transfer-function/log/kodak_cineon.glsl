//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Kodak Cineon)

float kodak_cineon(float x) {
    const float a = 685.0;
    const float b = 300.0;
    const float c = 95.0;
    const float off = pow(10.0, (c - a) / b);

    return (a + b * log(x * (1.0 - off) + off) / log(10.0)) / 1023.0;
}

vec3 kodak_cineon(vec3 color) {
    return vec3(
        kodak_cineon(color.r),
        kodak_cineon(color.g),
        kodak_cineon(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = kodak_cineon(color.rgb);

    return color;
}
