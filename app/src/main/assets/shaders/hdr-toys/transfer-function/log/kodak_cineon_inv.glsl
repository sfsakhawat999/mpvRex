//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Kodak Cineon, inverse)

float kodak_cineon_inv(float x) {
    const float a = 685.0;
    const float b = 300.0;
    const float c = 95.0;
    const float off = pow(10.0, (c - a) / b);

    return (pow(10.0, (1023.0 * x - a) / b) - off) / (1.0 - off);
}

vec3 kodak_cineon_inv(vec3 color) {
    return vec3(
        kodak_cineon_inv(color.r),
        kodak_cineon_inv(color.g),
        kodak_cineon_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = kodak_cineon_inv(color.rgb);

    return color;
}
