package game.server.command;

import core.assets.AssetManager;
import core.utils.MathUtils;
import core.utils.Vector3l;

import game.assets.StructureIdentifier;
import game.player.Player;
import game.player.interaction.PlacingState;
import game.player.interaction.RepeatPlaceable;
import game.player.interaction.StructureSelector;
import game.player.interaction.Target;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.materials_data.MaterialsData;
import game.server.saving.StructureSaver;
import game.utils.Utils;

import java.io.File;

import static game.utils.Constants.*;

public final class StructureCommand {

    static final String SYNTAX = "save \"Structure Name\" [force]";
    static final String EXPLANATION = "Saves the selected Region as a structure with the given name";

    static CommandResult execute(TokenList tokens) {
        KeywordToken action = tokens.expectNextKeyWord();
        if ("save".equalsIgnoreCase(action.keyword())) return executeSaveAction(tokens);
        return CommandResult.fail("unexpected keyword: " + action.keyword());
    }

    private static CommandResult executeSaveAction(TokenList tokens) {
        String fileName = Utils.sanitizeFileName(tokens.expectNextString().string());
        String saveFileLocation = StructureSaver.getSaveFileLocation(fileName);
        boolean forceSave;
        if (tokens.isFinished()) forceSave = false;
        else {
            String keyword = tokens.expectNextKeyWord().keyword();
            if ("force".equalsIgnoreCase(keyword)) forceSave = true;
            else return CommandResult.fail("Unexpected keyword: " + keyword);
        }
        if (!forceSave && new File(saveFileLocation).exists())
            return CommandResult.fail("That structure already exists. Choose another name or override with /%s force".formatted(tokens.getCommand()));

        tokens.expectFinishedLess();

        Player player = Game.getPlayer();
        Target lockedTarget = player.getInteractionHandler().getLockedTarget();
        Target startTarget = player.getInteractionHandler().getStartTarget();
        if (startTarget == null) startTarget = lockedTarget;
        PlacingState state = player.getInteractionHandler().getState(Target.getPlayerTarget());

        if (!(player.getHeldPlaceable() instanceof StructureSelector))
            return CommandResult.fail("Must use a Structure Selector");
        if (state != PlacingState.STRUCTURE_SELECT_LOCKED)
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

        structureSaver.save(structure, saveFileLocation);
        AssetManager.delete(new StructureIdentifier(fileName));
        return CommandResult.success();
    }
}
