#version 400 core
#define MAX_TEXT_LENGTH 128

in int data;

out vec2 textureCoordinates;

uniform ivec2 screenSize;
uniform vec2 charSize;
uniform int[MAX_TEXT_LENGTH] string;
uniform int[MAX_TEXT_LENGTH + 1] offsets;
uniform vec2 position;

void main() {
    vec2 scale = 2 * charSize / screenSize;
    int index = data & MAX_TEXT_LENGTH - 1;
    int charPixelWidth = offsets[index + 1] - offsets[index];

    float deltaX = (data >> 8 & 1) * charPixelWidth / 7.0;
    float deltaY = data >> 9 & 1;

    float x = 2 * position.x + (offsets[index] / 7.0 + deltaX) * scale.x;
    float y = 2 * position.y + deltaY * scale.y;
    gl_Position = vec4(x, y, 0.5, 1);

    int character = string[index];
    float u = ((character & 15) + deltaX) * 0.0625;
    float v = ((character >> 4 & 15) + 1 - deltaY) * 0.0625;
    textureCoordinates = vec2(u, v);
}