//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (GoPro Protune, inverse)

float gopro_protune_inv(float x) {
    return (pow(113.0, x) - 1.0) / 112.0;
}

vec3 gopro_protune_inv(vec3 color) {
    return vec3(
        gopro_protune_inv(color.r),
        gopro_protune_inv(color.g),
        gopro_protune_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = gopro_protune_inv(color.rgb);

    return color;
}
