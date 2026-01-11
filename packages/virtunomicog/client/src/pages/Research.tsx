import { useState } from "react";
import { trpc } from "@/lib/trpc";
import GameLayout from "@/components/GameLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Progress } from "@/components/ui/progress";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Loader2, FlaskConical, Beaker, Lightbulb, CheckCircle2, Lock, ArrowRight, TrendingUp, Zap, Users, Package, DollarSign } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

type TechnologyEffect = {
  type: string;
  value: number;
};

export default function Research() {
  const { toast } = useToast();
  const [selectedTech, setSelectedTech] = useState<number | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const { data: allTechnologies, isLoading: techLoading } = trpc.technology.all.useQuery();
  const { data: myTechnologies, refetch: refetchMyTech } = trpc.technology.mine.useQuery();
  const { data: availableTechnologies, refetch: refetchAvailable } = trpc.technology.available.useQuery();

  const startResearch = trpc.technology.startResearch.useMutation({
    onSuccess: (result) => {
      if (result.success) {
        toast({
          title: "Research Started",
          description: result.message,
        });
        refetchMyTech();
        refetchAvailable();
        setDialogOpen(false);
      } else {
        toast({
          title: "Cannot Start Research",
          description: result.message,
          variant: "destructive",
        });
      }
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case "production":
        return <Package className="w-5 h-5" />;
      case "commerce":
        return <DollarSign className="w-5 h-5" />;
      case "management":
        return <Users className="w-5 h-5" />;
      case "science":
        return <FlaskConical className="w-5 h-5" />;
      default:
        return <Lightbulb className="w-5 h-5" />;
    }
  };

  const getCategoryColor = (category: string) => {
    switch (category) {
      case "production":
        return "bg-blue-500/20 text-blue-500 border-blue-500/50";
      case "commerce":
        return "bg-green-500/20 text-green-500 border-green-500/50";
      case "management":
        return "bg-purple-500/20 text-purple-500 border-purple-500/50";
      case "science":
        return "bg-orange-500/20 text-orange-500 border-orange-500/50";
      default:
        return "";
    }
  };

  const formatEffect = (effect: TechnologyEffect) => {
    const percentage = (effect.value * 100).toFixed(0);
    switch (effect.type) {
      case "production_efficiency":
        return `+${percentage}% Production Efficiency`;
      case "labor_reduction":
        return `-${percentage}% Labor Required`;
      case "quality_bonus":
        return `+${percentage}% Output Quality`;
      case "sale_price_bonus":
        return `+${percentage}% Sale Prices`;
      case "shipping_cost_reduction":
        return `-${percentage}% Shipping Costs`;
      case "shipping_time_reduction":
        return `-${percentage}% Shipping Time`;
      case "reliability_bonus":
        return `+${percentage}% Route Reliability`;
      case "qualification_growth":
        return `+${percentage}% Employee Training`;
      case "maintenance_reduction":
        return `-${percentage}% Maintenance Costs`;
      case "morale_bonus":
        return `+${percentage}% Employee Morale`;
      case "research_speed":
        return `+${percentage}% Research Speed`;
      case "unlock_recipes":
        return "Unlocks Advanced Recipes";
      case "operating_cost_reduction":
        return `-${percentage}% Operating Costs`;
      case "reputation_bonus":
        return `+${effect.value} Reputation`;
      default:
        return `${effect.type}: ${effect.value}`;
    }
  };

  const getTechStatus = (techId: number) => {
    const myTech = myTechnologies?.find((t) => t.technologyId === techId);
    if (!myTech) return "available";
    if (myTech.isCompleted) return "completed";
    return "in_progress";
  };

  const getTechProgress = (techId: number) => {
    const myTech = myTechnologies?.find((t) => t.technologyId === techId);
    if (!myTech) return 0;
    const tech = allTechnologies?.find((t) => t.id === techId);
    if (!tech) return 0;
    return (myTech.researchProgress / tech.researchCost) * 100;
  };

  const isAvailable = (techId: number) => {
    return availableTechnologies?.some((t) => t.id === techId);
  };

  const selectedTechData = allTechnologies?.find((t) => t.id === selectedTech);

  const handleStartResearch = () => {
    if (!selectedTech) return;
    startResearch.mutate({ technologyId: selectedTech });
  };

  // Group technologies by category
  const techByCategory = allTechnologies?.reduce(
    (acc, tech) => {
      if (!acc[tech.category]) acc[tech.category] = [];
      acc[tech.category].push(tech);
      return acc;
    },
    {} as Record<string, typeof allTechnologies>
  );

  // Count stats
  const completedCount = myTechnologies?.filter((t) => t.isCompleted).length || 0;
  const inProgressCount = myTechnologies?.filter((t) => !t.isCompleted).length || 0;
  const totalCount = allTechnologies?.length || 0;

  if (techLoading) {
    return (
      <GameLayout>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </GameLayout>
    );
  }

  return (
    <GameLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Technology Research</h1>
          <p className="text-muted-foreground">Research new technologies to improve your business</p>
        </div>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-blue-500/20">
                  <Beaker className="w-6 h-6 text-blue-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total Technologies</p>
                  <p className="text-2xl font-bold">{totalCount}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-green-500/20">
                  <CheckCircle2 className="w-6 h-6 text-green-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Completed</p>
                  <p className="text-2xl font-bold">{completedCount}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-orange-500/20">
                  <FlaskConical className="w-6 h-6 text-orange-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">In Progress</p>
                  <p className="text-2xl font-bold">{inProgressCount}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-full bg-purple-500/20">
                  <Lightbulb className="w-6 h-6 text-purple-500" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Available</p>
                  <p className="text-2xl font-bold">{availableTechnologies?.length || 0}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <Tabs defaultValue="all">
          <TabsList>
            <TabsTrigger value="all">All Technologies</TabsTrigger>
            <TabsTrigger value="available">Available</TabsTrigger>
            <TabsTrigger value="progress">In Progress</TabsTrigger>
            <TabsTrigger value="completed">Completed</TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-6">
            {techByCategory && Object.entries(techByCategory).map(([category, techs]) => (
              <Card key={category}>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 capitalize">
                    {getCategoryIcon(category)}
                    {category} Technologies
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {techs?.map((tech) => {
                      const status = getTechStatus(tech.id);
                      const progress = getTechProgress(tech.id);
                      const available = isAvailable(tech.id);
                      const effects = (tech.effects as TechnologyEffect[] | null) || [];

                      return (
                        <div
                          key={tech.id}
                          className={`p-4 rounded-lg border bg-card transition-colors ${
                            status === "completed"
                              ? "border-green-500/50"
                              : status === "in_progress"
                              ? "border-orange-500/50"
                              : available
                              ? "hover:border-primary/50 cursor-pointer"
                              : "opacity-60"
                          }`}
                          onClick={() => {
                            if (available && status === "available") {
                              setSelectedTech(tech.id);
                              setDialogOpen(true);
                            }
                          }}
                        >
                          <div className="flex items-start justify-between mb-2">
                            <div className="flex items-center gap-2">
                              {status === "completed" ? (
                                <CheckCircle2 className="w-5 h-5 text-green-500" />
                              ) : status === "in_progress" ? (
                                <FlaskConical className="w-5 h-5 text-orange-500" />
                              ) : !available ? (
                                <Lock className="w-5 h-5 text-muted-foreground" />
                              ) : (
                                getCategoryIcon(category)
                              )}
                              <h4 className="font-semibold">{tech.name}</h4>
                            </div>
                            <Badge className={getCategoryColor(category)}>
                              {category}
                            </Badge>
                          </div>

                          <p className="text-sm text-muted-foreground mb-3">
                            {tech.description}
                          </p>

                          {status === "in_progress" && (
                            <div className="mb-3">
                              <div className="flex items-center justify-between text-sm mb-1">
                                <span className="text-muted-foreground">Progress</span>
                                <span className="font-medium">{progress.toFixed(0)}%</span>
                              </div>
                              <Progress value={progress} className="h-2" />
                            </div>
                          )}

                          <div className="flex items-center justify-between">
                            <div className="flex flex-wrap gap-1">
                              {effects.slice(0, 2).map((effect, idx) => (
                                <Badge key={idx} variant="outline" className="text-xs">
                                  <TrendingUp className="w-3 h-3 mr-1" />
                                  {formatEffect(effect)}
                                </Badge>
                              ))}
                            </div>
                            {status === "available" && available && (
                              <Button size="sm" variant="outline">
                                <Zap className="w-4 h-4 mr-1" />
                                Research
                              </Button>
                            )}
                          </div>

                          <div className="mt-2 text-xs text-muted-foreground">
                            Research Cost: {tech.researchCost} points
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            ))}
          </TabsContent>

          <TabsContent value="available">
            <Card>
              <CardHeader>
                <CardTitle>Available for Research</CardTitle>
                <CardDescription>Technologies you can start researching now</CardDescription>
              </CardHeader>
              <CardContent>
                {availableTechnologies && availableTechnologies.length > 0 ? (
                  <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {availableTechnologies.map((tech) => {
                      const effects = (tech.effects as TechnologyEffect[] | null) || [];
                      return (
                        <Dialog key={tech.id} open={dialogOpen && selectedTech === tech.id} onOpenChange={(open) => {
                          setDialogOpen(open);
                          if (open) setSelectedTech(tech.id);
                        }}>
                          <DialogTrigger asChild>
                            <div className="p-4 rounded-lg border bg-card hover:border-primary/50 cursor-pointer transition-colors">
                              <div className="flex items-center gap-2 mb-2">
                                {getCategoryIcon(tech.category)}
                                <h4 className="font-semibold">{tech.name}</h4>
                              </div>
                              <p className="text-sm text-muted-foreground mb-3">
                                {tech.description}
                              </p>
                              <div className="flex flex-wrap gap-1 mb-2">
                                {effects.map((effect, idx) => (
                                  <Badge key={idx} variant="outline" className="text-xs">
                                    {formatEffect(effect)}
                                  </Badge>
                                ))}
                              </div>
                              <div className="flex items-center justify-between">
                                <Badge className={getCategoryColor(tech.category)}>
                                  {tech.category}
                                </Badge>
                                <span className="text-sm text-muted-foreground">
                                  {tech.researchCost} points
                                </span>
                              </div>
                            </div>
                          </DialogTrigger>
                          <DialogContent>
                            <DialogHeader>
                              <DialogTitle className="flex items-center gap-2">
                                {getCategoryIcon(tech.category)}
                                {tech.name}
                              </DialogTitle>
                              <DialogDescription>{tech.description}</DialogDescription>
                            </DialogHeader>
                            <div className="space-y-4 py-4">
                              <div className="p-4 rounded-lg bg-muted">
                                <h4 className="font-medium mb-2">Effects</h4>
                                <div className="space-y-2">
                                  {effects.map((effect, idx) => (
                                    <div key={idx} className="flex items-center gap-2">
                                      <TrendingUp className="w-4 h-4 text-green-500" />
                                      <span>{formatEffect(effect)}</span>
                                    </div>
                                  ))}
                                </div>
                              </div>
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-muted-foreground">Research Cost:</span>
                                <span className="font-medium">{tech.researchCost} research points</span>
                              </div>
                            </div>
                            <DialogFooter>
                              <Button variant="outline" onClick={() => setDialogOpen(false)}>
                                Cancel
                              </Button>
                              <Button onClick={handleStartResearch} disabled={startResearch.isPending}>
                                {startResearch.isPending ? (
                                  <>
                                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                    Starting...
                                  </>
                                ) : (
                                  <>
                                    <FlaskConical className="w-4 h-4 mr-2" />
                                    Start Research
                                  </>
                                )}
                              </Button>
                            </DialogFooter>
                          </DialogContent>
                        </Dialog>
                      );
                    })}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12">
                    <Lock className="w-12 h-12 text-muted-foreground mb-4" />
                    <h3 className="text-lg font-semibold mb-2">No Technologies Available</h3>
                    <p className="text-muted-foreground text-center">
                      Complete prerequisite technologies to unlock more research options.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="progress">
            <Card>
              <CardHeader>
                <CardTitle>Research In Progress</CardTitle>
                <CardDescription>Technologies currently being researched</CardDescription>
              </CardHeader>
              <CardContent>
                {myTechnologies?.filter((t) => !t.isCompleted).length ? (
                  <div className="space-y-4">
                    {myTechnologies.filter((t) => !t.isCompleted).map((myTech) => {
                      const tech = allTechnologies?.find((t) => t.id === myTech.technologyId);
                      if (!tech) return null;
                      const progress = (myTech.researchProgress / tech.researchCost) * 100;
                      const effects = (tech.effects as TechnologyEffect[] | null) || [];

                      return (
                        <div key={myTech.id} className="p-4 rounded-lg border bg-card">
                          <div className="flex items-start justify-between mb-3">
                            <div>
                              <div className="flex items-center gap-2">
                                {getCategoryIcon(tech.category)}
                                <h4 className="font-semibold">{tech.name}</h4>
                              </div>
                              <p className="text-sm text-muted-foreground mt-1">
                                {tech.description}
                              </p>
                            </div>
                            <Badge className={getCategoryColor(tech.category)}>
                              {tech.category}
                            </Badge>
                          </div>

                          <div className="mb-3">
                            <div className="flex items-center justify-between text-sm mb-1">
                              <span className="text-muted-foreground">
                                {myTech.researchProgress} / {tech.researchCost} points
                              </span>
                              <span className="font-medium">{progress.toFixed(0)}%</span>
                            </div>
                            <Progress value={progress} className="h-3" />
                          </div>

                          <div className="flex flex-wrap gap-1">
                            {effects.map((effect, idx) => (
                              <Badge key={idx} variant="outline" className="text-xs">
                                {formatEffect(effect)}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12">
                    <FlaskConical className="w-12 h-12 text-muted-foreground mb-4" />
                    <h3 className="text-lg font-semibold mb-2">No Active Research</h3>
                    <p className="text-muted-foreground text-center">
                      Start researching a technology from the Available tab.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="completed">
            <Card>
              <CardHeader>
                <CardTitle>Completed Research</CardTitle>
                <CardDescription>Technologies you have mastered</CardDescription>
              </CardHeader>
              <CardContent>
                {myTechnologies?.filter((t) => t.isCompleted).length ? (
                  <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {myTechnologies.filter((t) => t.isCompleted).map((myTech) => {
                      const tech = allTechnologies?.find((t) => t.id === myTech.technologyId);
                      if (!tech) return null;
                      const effects = (tech.effects as TechnologyEffect[] | null) || [];

                      return (
                        <div key={myTech.id} className="p-4 rounded-lg border border-green-500/50 bg-card">
                          <div className="flex items-center gap-2 mb-2">
                            <CheckCircle2 className="w-5 h-5 text-green-500" />
                            <h4 className="font-semibold">{tech.name}</h4>
                          </div>
                          <p className="text-sm text-muted-foreground mb-3">
                            {tech.description}
                          </p>
                          <div className="flex flex-wrap gap-1 mb-2">
                            {effects.map((effect, idx) => (
                              <Badge key={idx} variant="outline" className="text-xs text-green-500">
                                {formatEffect(effect)}
                              </Badge>
                            ))}
                          </div>
                          <div className="flex items-center justify-between text-sm">
                            <Badge className={getCategoryColor(tech.category)}>
                              {tech.category}
                            </Badge>
                            {myTech.completedAt && (
                              <span className="text-muted-foreground">
                                Completed {new Date(myTech.completedAt).toLocaleDateString()}
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12">
                    <CheckCircle2 className="w-12 h-12 text-muted-foreground mb-4" />
                    <h3 className="text-lg font-semibold mb-2">No Completed Research</h3>
                    <p className="text-muted-foreground text-center">
                      Completed technologies will appear here.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </GameLayout>
  );
}
