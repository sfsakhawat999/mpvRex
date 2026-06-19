//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (GoPro Protune)

float gopro_protune(float x) {
    return log(x * 112.0 + 1.0) / log(113.0);
}

vec3 gopro_protune(vec3 color) {
    return vec3(
        gopro_protune(color.r),
        gopro_protune(color.g),
        gopro_protune(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = gopro_protune(color.rgb);

    return color;
}
