// Merged ko→en dictionary. Keys are the exact Korean strings used in the UI.
import { common } from "./common";
import { home } from "./home";
import { apply } from "./apply";
import { applyFlow } from "./applyFlow";
import { auth } from "./auth";
import { content } from "./content";
import { support } from "./support";
import { reviews } from "./reviews";

export const translations: Record<string, string> = {
  ...common,
  ...home,
  ...apply,
  ...applyFlow,
  ...auth,
  ...content,
  ...support,
  ...reviews,
};
