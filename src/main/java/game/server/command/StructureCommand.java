package game.server.command;

import core.utils.MathUtils;
import core.utils.Vector3l;

import game.player.Player;
import game.player.interaction.PlacingState;
import game.player.interaction.RepeatPlaceable;
import game.player.interaction.Target;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.materials_data.MaterialsData;
import game.server.saving.StructureSaver;
import game.utils.Utils;

import static game.utils.Constants.*;

public final class StructureCommand {

    static final String SYNTAX = "save \"Structure Name\"";
    static final String EXPLANATION = "Saves the selected Region as a structure with the given name";

    static CommandResult execute(TokenList tokens) {
        KeywordToken action = tokens.expectNextKeyWord();
        if ("save".equalsIgnoreCase(action.keyword())) return executeSaveAction(tokens);
        return CommandResult.fail("unexpected keyword: " + action.keyword());
    }

    private static CommandResult executeSaveAction(TokenList tokens) {
        StringToken name = tokens.expectNextString();
        tokens.expectFinishedLess();

        Player player = Game.getPlayer();
        Target lockedTarget = player.getInteractionHandler().getLockedTarget();
        Target startTarget = player.getInteractionHandler().getStartTarget();
        PlacingState state = player.getInteractionHandler().getState(Target.getPlayerTarget());

        if (player.getHeldPlaceable() != null)
            return CommandResult.fail("Must have an empty hand");
        if (state != PlacingState.REPEAT_BREAK_LOCKED)
            return CommandResult.fail("Must have locked a region to save as a structure");

        Vector3l startPositon = startTarget.position();
        Vector3l endPosition = lockedTarget.position();

        RepeatPlaceable.offsetPositions(startPositon, endPosition, startTarget.side(), null);
        Vector3l minPosition = Utils.min(startPositon, endPosition);
        Vector3l maxPosition = Utils.max(startPositon, endPosition);
        maxPosition.add(1, 1, 1);

        int sizeX = (int) (maxPosition.x - minPosition.x);
        int sizeY = (int) (maxPosition.y - minPosition.y);
        int sizeZ = (int) (maxPosition.z - minPosition.z);

        if (sizeX > MAX_STRUCTURE_SIZE || sizeY > MAX_STRUCTURE_SIZE || sizeZ > MAX_STRUCTURE_SIZE)
            return CommandResult.fail("Structure cannot be larget than %d voxels along one axis".formatted(MAX_STRUCTURE_SIZE));

        int sizeBits = Integer.numberOfTrailingZeros(MathUtils.nextLargestPowOf2(MathUtils.max(sizeX, sizeY, sizeZ)));
        byte[] uncompressedMaterials = new byte[1 << sizeBits * 3];

        World world = Game.getWorld();
        for (int structureX = 0; structureX < sizeX; structureX++)
            for (int structureY = 0; structureY < sizeY; structureY++)
                for (int structureZ = 0; structureZ < sizeZ; structureZ++) {
                    byte material = world.getMaterial(minPosition.x + structureX, minPosition.y + structureY, minPosition.z + structureZ, 0);
                    int index = MaterialsData.getUncompressedIndex(structureX, structureY, structureZ);
                    uncompressedMaterials[index] = material;
                }

        Structure structure = new Structure(sizeX, sizeY, sizeZ, MaterialsData.getCompressedMaterials(sizeBits, uncompressedMaterials));
        StructureSaver structureSaver = new StructureSaver();
        String fileName = Utils.sanitizeFileName(name.string());

        structureSaver.save(structure, StructureSaver.getSaveFileLocation(fileName));
        return CommandResult.success();
    }
}
