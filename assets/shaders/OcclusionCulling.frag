#version 460 core

flat in int instanceID;
layout(early_fragment_tests) in;

struct IndirectCommand {
uint count;
uint instanceCount;
uint first;
uint baseInstance;
};

layout (std430, binding = 1) restrict writeonly buffer opaqueIndirectBuffer {
    IndirectCommand[] opaqueCommands;
};
layout (std430, binding = 2) restrict writeonly buffer waterIndirectBuffer {
    IndirectCommand[] waterCommands;
};
layout (std430, binding = 3) restrict writeonly buffer glassIndirectBuffer {
    IndirectCommand[] glassCommands;
};

const int NORTH = 0;
const int TOP = 1;
const int WEST = 2;
const int SOUTH = 3;
const int BOTTOM = 4;
const int EAST = 5;

void main() {
    opaqueCommands[instanceID * 6 + NORTH].instanceCount = 1;
    opaqueCommands[instanceID * 6 + TOP].instanceCount = 1;
    opaqueCommands[instanceID * 6 + WEST].instanceCount = 1;
    opaqueCommands[instanceID * 6 + SOUTH].instanceCount = 1;
    opaqueCommands[instanceID * 6 + BOTTOM].instanceCount = 1;
    opaqueCommands[instanceID * 6 + EAST].instanceCount = 1;

    waterCommands[instanceID].instanceCount = 1;
    glassCommands[instanceID].instanceCount = 1;
}