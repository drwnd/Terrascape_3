#version 400 core

uniform sampler2D colorTexture;
uniform sampler2D ssaoTexture;
uniform ivec2 screenSize;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

float getAmbientOcclusion() {
    float occlusion = 0.0;
    vec2 texelSize = vec2(1.0 / float(screenSize.x), 1.0 / float(screenSize.y));

    for (int x = -2; x < 2; x++) {
        for (int y = -2; y < 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            occlusion += texture(ssaoTexture, fragTextureCoordinate + offset).r;
        }
    }
    return occlusion * 0.0625;
}

void main() {
    vec4 color = texture(colorTexture, fragTextureCoordinate);
    if (color.a == 0) discard;

    float occlusion = getAmbientOcclusion();
    fragColor = vec4(color.rgb * occlusion, color.a);
}